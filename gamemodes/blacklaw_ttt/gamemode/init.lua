AddCSLuaFile("shared.lua")
AddCSLuaFile("cl_init.lua")
AddCSLuaFile("client/cl_hud.lua")
AddCSLuaFile("client/cl_menu.lua")

include("shared.lua")
include("server/sv_net.lua")
include("server/sv_roles.lua")
include("server/sv_rounds.lua")

BL.ServerReady = true
