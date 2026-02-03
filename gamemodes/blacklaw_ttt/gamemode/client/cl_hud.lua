BL.ClientState = BL.ClientState or {
  role = BL.Roles.INNOCENT,
  credits = 0,
  roundState = BL.RoundStates.WAITING,
  roundEndsAt = 0,
}

local function palette()
  return {
    bg = Color(12, 14, 18, 235),
    panel = Color(20, 24, 33, 235),
    highlight = Color(96, 184, 255),
    text = Color(236, 240, 246),
    muted = Color(142, 153, 171),
  }
end

net.Receive(BL.NetMessages.RoundState, function()
  local state = net.ReadUInt(3)
  local endsAt = net.ReadFloat()
  BL.ClientState.roundState = state
  BL.ClientState.roundEndsAt = endsAt
end)

net.Receive(BL.NetMessages.RoleState, function()
  BL.ClientState.role = net.ReadUInt(3)
  BL.ClientState.credits = net.ReadUInt(8)
end)

hook.Add("HUDPaint", "BL.HUDPaint", function()
  local colors = palette()
  local sw, sh = ScrW(), ScrH()
  local padding = 16
  local cardW, cardH = 320, 96
  local x = padding
  local y = sh - cardH - padding

  draw.RoundedBox(12, x, y, cardW, cardH, colors.bg)
  draw.RoundedBox(10, x + 6, y + 6, cardW - 12, cardH - 12, colors.panel)

  local roleData = BL.GetRoleData(BL.ClientState.role)
  draw.SimpleText("BLACKLAW TTT", "Trebuchet18", x + 20, y + 18, colors.muted)
  draw.SimpleText(roleData.name, "Trebuchet24", x + 20, y + 40, roleData.color)
  draw.SimpleText("Credits: " .. tostring(BL.ClientState.credits), "Trebuchet18", x + 20, y + 64, colors.text)

  local stateName = BL.GetRoundStateName(BL.ClientState.roundState)
  local timeLeft = 0
  if BL.ClientState.roundEndsAt and BL.ClientState.roundEndsAt > 0 then
    timeLeft = math.max(0, math.ceil(BL.ClientState.roundEndsAt - CurTime()))
  end

  local timerText = timeLeft > 0 and (stateName .. " • " .. timeLeft .. "s") or stateName
  draw.SimpleText(timerText, "Trebuchet18", x + cardW - 20, y + 18, colors.muted, TEXT_ALIGN_RIGHT)
end)
