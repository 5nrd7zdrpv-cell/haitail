BL.Menu = BL.Menu or {}

local function palette()
  return {
    bg = Color(12, 14, 18, 240),
    panel = Color(20, 24, 33, 235),
    accent = Color(96, 184, 255),
    accentSoft = Color(96, 184, 255, 40),
    text = Color(236, 240, 246),
    muted = Color(142, 153, 171),
    danger = Color(235, 80, 80),
  }
end

local function drawCard(panel, w, h)
  local colors = palette()
  draw.RoundedBox(12, 0, 0, w, h, colors.panel)
  draw.RoundedBox(12, 2, 2, w - 4, h - 4, colors.bg)
end

local function makeButton(parent, text, onClick)
  local colors = palette()
  local btn = vgui.Create("DButton", parent)
  btn:SetText("")
  btn:SetTall(44)
  btn.Paint = function(self, w, h)
    draw.RoundedBox(10, 0, 0, w, h, colors.accentSoft)
    if self:IsHovered() then
      draw.RoundedBox(10, 0, 0, w, h, Color(96, 184, 255, 90))
    end
    draw.SimpleText(text, "Trebuchet20", w * 0.5, h * 0.5, colors.text, TEXT_ALIGN_CENTER, TEXT_ALIGN_CENTER)
  end
  btn.DoClick = onClick
  return btn
end

function BL.Menu.OpenShop()
  if IsValid(BL.Menu.Frame) then
    BL.Menu.Frame:Remove()
  end

  local colors = palette()
  local frame = vgui.Create("DFrame")
  BL.Menu.Frame = frame
  frame:SetSize(520, 520)
  frame:Center()
  frame:SetTitle("")
  frame:ShowCloseButton(false)
  frame:MakePopup()
  frame.Paint = function(self, w, h)
    draw.RoundedBox(16, 0, 0, w, h, colors.bg)
    draw.SimpleText("Blacklaw Arsenal", "Trebuchet24", 24, 20, colors.text)
    draw.SimpleText("Authoritative server shop", "Trebuchet18", 24, 48, colors.muted)
  end

  local close = makeButton(frame, "Close", function()
    frame:Remove()
  end)
  close:SetWide(100)
  close:SetPos(frame:GetWide() - 120, 20)

  local scroll = vgui.Create("DScrollPanel", frame)
  scroll:SetPos(24, 90)
  scroll:SetSize(frame:GetWide() - 48, frame:GetTall() - 114)
  scroll.Paint = function() end

  for _, item in ipairs(BL.Items) do
    local card = vgui.Create("DPanel", scroll)
    card:Dock(TOP)
    card:DockMargin(0, 0, 0, 12)
    card:SetTall(90)
    card.Paint = function(self, w, h)
      drawCard(self, w, h)
      draw.SimpleText(item.name, "Trebuchet20", 20, 18, colors.text)
      draw.SimpleText(item.desc, "Trebuchet18", 20, 46, colors.muted)
      draw.SimpleText("Cost: " .. item.cost, "Trebuchet18", w - 20, 18, colors.accent, TEXT_ALIGN_RIGHT)
    end

    local buy = makeButton(card, "Buy", function()
      net.Start(BL.NetMessages.BuyRequest)
      net.WriteString(item.id)
      net.SendToServer()
    end)
    buy:SetWide(80)
    buy:SetPos(card:GetWide() - 100, 44)
    buy.Think = function(self)
      self:SetPos(card:GetWide() - 100, 44)
    end
  end
end

hook.Add("OnContextMenuOpen", "BL.OpenShop", function()
  BL.Menu.OpenShop()
end)
