BL.PlayerState = BL.PlayerState or {}

function BL.GetPlayerState(ply)
  BL.PlayerState[ply] = BL.PlayerState[ply] or {
    role = BL.Roles.INNOCENT,
    credits = 0,
  }
  return BL.PlayerState[ply]
end

function BL.SetPlayerRole(ply, role)
  local state = BL.GetPlayerState(ply)
  local roleData = BL.GetRoleData(role)
  state.role = role
  state.credits = BL.Config.StartingCredits[roleData.name:upper()] or 0

  net.Start(BL.NetMessages.RoleState)
  net.WriteUInt(role, 3)
  net.WriteUInt(state.credits, 8)
  net.Send(ply)
end

function BL.BroadcastRoundState()
  net.Start(BL.NetMessages.RoundState)
  net.WriteUInt(BL.RoundState or BL.RoundStates.WAITING, 3)
  net.WriteFloat(BL.RoundEndsAt or 0)
  net.Broadcast()
end

function BL.AssignRoles()
  local players = player.GetAll()
  local total = #players
  if total == 0 then
    return
  end

  local traitorCount = math.max(1, math.floor(total / 4))
  local detectiveCount = total >= 4 and 1 or 0

  local shuffled = {}
  for _, ply in ipairs(players) do
    if IsValid(ply) and ply:IsPlayer() then
      table.insert(shuffled, ply)
    end
  end

  for i = #shuffled, 2, -1 do
    local j = math.random(i)
    shuffled[i], shuffled[j] = shuffled[j], shuffled[i]
  end

  local index = 1
  for i = 1, traitorCount do
    local ply = shuffled[index]
    if IsValid(ply) then
      BL.SetPlayerRole(ply, BL.Roles.TRAITOR)
    end
    index = index + 1
  end

  for i = 1, detectiveCount do
    local ply = shuffled[index]
    if IsValid(ply) then
      BL.SetPlayerRole(ply, BL.Roles.DETECTIVE)
    end
    index = index + 1
  end

  for i = index, #shuffled do
    local ply = shuffled[i]
    if IsValid(ply) then
      BL.SetPlayerRole(ply, BL.Roles.INNOCENT)
    end
  end
end

function BL.HandleBuyRequest(ply, id)
  if not IsValid(ply) or not ply:IsPlayer() then
    return
  end

  local state = BL.GetPlayerState(ply)
  if BL.RoundState ~= BL.RoundStates.ACTIVE then
    return
  end

  local item = BL.FindItem(id)
  if not item then
    return
  end

  if not item.allowed[state.role] then
    return
  end

  if state.credits < item.cost then
    return
  end

  state.credits = state.credits - item.cost

  if id == "kevlar" then
    ply:SetArmor(math.min(100, ply:Armor() + 50))
  elseif id == "medkit" then
    ply:SetHealth(math.min(ply:GetMaxHealth(), ply:Health() + 25))
  end

  net.Start(BL.NetMessages.RoleState)
  net.WriteUInt(state.role, 3)
  net.WriteUInt(state.credits, 8)
  net.Send(ply)
end

hook.Add("PlayerSpawn", "BL.AssignDefaultRole", function(ply)
  if not IsValid(ply) then
    return
  end

  if BL.RoundState ~= BL.RoundStates.ACTIVE then
    BL.SetPlayerRole(ply, BL.Roles.INNOCENT)
  end
end)

hook.Add("PlayerDisconnected", "BL.RoleCleanup", function(ply)
  BL.PlayerState[ply] = nil
end)
