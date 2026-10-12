execute store result storage emi_plush:gasha arg.i int 1 run random value 0..10
data modify storage emi_plush:gasha arg.pool set value "comun"
function emi_plush:gasha/elegir with storage emi_plush:gasha arg
data modify storage emi_plush:gasha res set value {}
data modify storage emi_plush:gasha res.pool set value "comun"
data modify storage emi_plush:gasha res.sel set from storage emi_plush:gasha sel
function emi_plush:gasha/fx_comun
