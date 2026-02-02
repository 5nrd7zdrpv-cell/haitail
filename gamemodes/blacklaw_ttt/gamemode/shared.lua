BL = BL or {}

BL.Version = "0.1.0"

BL.Config = {
  MinPlayers = 2,
  PrepTime = 15,
  RoundTime = 240,
  PostTime = 10,
  StartingCredits = {
    ["TRAITOR"] = 1,
    ["DETECTIVE"] = 1,
    ["INNOCENT"] = 0,
  },
}

BL.Roles = {
  INNOCENT = 1,
  TRAITOR = 2,
  DETECTIVE = 3,
}

BL.RoleData = {
  [BL.Roles.INNOCENT] = {
    name = "Innocent",
    short = "INN",
    color = Color(88, 204, 112),
  },
  [BL.Roles.TRAITOR] = {
    name = "Traitor",
    short = "TRT",
    color = Color(235, 80, 80),
  },
  [BL.Roles.DETECTIVE] = {
    name = "Detective",
    short = "DET",
    color = Color(88, 160, 235),
  },
}

BL.RoundStates = {
  WAITING = 1,
  PREP = 2,
  ACTIVE = 3,
  POST = 4,
}

BL.RoundStateNames = {
  [BL.RoundStates.WAITING] = "Waiting",
  [BL.RoundStates.PREP] = "Preparation",
  [BL.RoundStates.ACTIVE] = "Active",
  [BL.RoundStates.POST] = "Post",
}

BL.NetMessages = {
  RoundState = "bl_ttt_round_state",
  RoleState = "bl_ttt_role_state",
  BuyRequest = "bl_ttt_buy_request",
}

BL.Items = {
  {
    id = "kevlar",
    name = "Kevlar Vest",
    cost = 1,
    desc = "Gain 50 armor immediately.",
    allowed = {
      [BL.Roles.TRAITOR] = true,
      [BL.Roles.DETECTIVE] = true,
    },
  },
  {
    id = "medkit",
    name = "Field Medkit",
    cost = 1,
    desc = "Heal 25 health instantly.",
    allowed = {
      [BL.Roles.TRAITOR] = true,
      [BL.Roles.DETECTIVE] = true,
      [BL.Roles.INNOCENT] = true,
    },
  },
}

function BL.GetRoleData(role)
  return BL.RoleData[role] or BL.RoleData[BL.Roles.INNOCENT]
end

function BL.GetRoundStateName(state)
  return BL.RoundStateNames[state] or "Unknown"
end

function BL.FindItem(id)
  if type(id) ~= "string" then
    return nil
  end

  for _, item in ipairs(BL.Items) do
    if item.id == id then
      return item
    end
  end

  return nil
end

function BL.IsValidItemId(id)
  if type(id) ~= "string" then
    return false
  end

  if #id == 0 or #id > 32 then
    return false
  end

  return id:match("^[%w_]+$") ~= nil
end
