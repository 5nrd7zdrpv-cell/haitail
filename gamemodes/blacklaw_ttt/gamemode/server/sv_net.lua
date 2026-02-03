BL.Net = BL.Net or {}
BL.NetLimiter = BL.NetLimiter or {}

for _, msg in pairs(BL.NetMessages) do
  util.AddNetworkString(msg)
end

function BL.Net.CanReceive(ply, key, interval)
  if not IsValid(ply) or not ply:IsPlayer() then
    return false
  end

  local now = CurTime()
  BL.NetLimiter[ply] = BL.NetLimiter[ply] or {}
  local nextAllowed = BL.NetLimiter[ply][key] or 0

  if now < nextAllowed then
    return false
  end

  BL.NetLimiter[ply][key] = now + interval
  return true
end

hook.Add("PlayerDisconnected", "BL.NetCleanup", function(ply)
  BL.NetLimiter[ply] = nil
end)

net.Receive(BL.NetMessages.BuyRequest, function(_, ply)
  if not BL.Net.CanReceive(ply, "buy", 0.4) then
    return
  end

  local id = net.ReadString()
  if not BL.IsValidItemId(id) then
    return
  end

  BL.HandleBuyRequest(ply, id)
end)
