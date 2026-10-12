execute store result storage emi_plush:gasha arg.i int 1 run random value 0..114
data modify storage emi_plush:gasha arg.pool set value "pareja"
function emi_plush:gasha/elegir with storage emi_plush:gasha arg
data modify storage emi_plush:gasha res set value {}
data modify storage emi_plush:gasha res.pool set value "pareja"
data modify storage emi_plush:gasha res.sel set from storage emi_plush:gasha sel
function emi_plush:gasha/fx_pareja
function emi_plush:gasha/anunciar with storage emi_plush:gasha sel
scoreboard players set @s emi_gasha 0
