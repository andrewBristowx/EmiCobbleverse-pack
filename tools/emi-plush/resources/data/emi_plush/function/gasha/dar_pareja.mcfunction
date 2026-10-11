execute store result storage emi_plush:gasha arg.i int 1 run random value 0..114
data modify storage emi_plush:gasha arg.pool set value "pareja"
function emi_plush:gasha/elegir with storage emi_plush:gasha arg
function emi_plush:gasha/fx_pareja
function emi_plush:gasha/anunciar with storage emi_plush:gasha sel
scoreboard players set @s emi_gasha 0
