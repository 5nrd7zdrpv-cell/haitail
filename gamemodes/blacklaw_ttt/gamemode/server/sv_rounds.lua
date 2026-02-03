BL.RoundState = BL.RoundState or BL.RoundStates.WAITING
BL.RoundEndsAt = BL.RoundEndsAt or 0

function BL.SetRoundState(state, duration)
  BL.RoundState = state
  if duration and duration > 0 then
    BL.RoundEndsAt = CurTime() + duration
  else
    BL.RoundEndsAt = 0
  end
  BL.BroadcastRoundState()
end

function BL.StartPrep()
  BL.SetRoundState(BL.RoundStates.PREP, BL.Config.PrepTime)
  BL.AssignRoles()
end

function BL.StartActive()
  BL.SetRoundState(BL.RoundStates.ACTIVE, BL.Config.RoundTime)
end

function BL.StartPost()
  BL.SetRoundState(BL.RoundStates.POST, BL.Config.PostTime)
end

function BL.StartWaiting()
  BL.SetRoundState(BL.RoundStates.WAITING, 0)
end

function BL.CanStartRound()
  return #player.GetAll() >= BL.Config.MinPlayers
end

function BL.TickRound()
  if BL.RoundState == BL.RoundStates.WAITING then
    if BL.CanStartRound() then
      BL.StartPrep()
    end
  elseif BL.RoundState == BL.RoundStates.PREP then
    if CurTime() >= BL.RoundEndsAt then
      BL.StartActive()
    end
  elseif BL.RoundState == BL.RoundStates.ACTIVE then
    if CurTime() >= BL.RoundEndsAt then
      BL.StartPost()
    end
  elseif BL.RoundState == BL.RoundStates.POST then
    if CurTime() >= BL.RoundEndsAt then
      if BL.CanStartRound() then
        BL.StartPrep()
      else
        BL.StartWaiting()
      end
    end
  end
end

hook.Add("Think", "BL.RoundTick", BL.TickRound)

hook.Add("PlayerInitialSpawn", "BL.SyncPlayerState", function(ply)
  if not IsValid(ply) then
    return
  end

  local state = BL.GetPlayerState(ply)
  net.Start(BL.NetMessages.RoleState)
  net.WriteUInt(state.role, 3)
  net.WriteUInt(state.credits, 8)
  net.Send(ply)

  timer.Simple(0.2, function()
    if IsValid(ply) then
      BL.BroadcastRoundState()
    end
  end)
end)
