scoreboard players add @s emi_gasha 1
execute if score @s emi_gasha matches 10.. run return run function emi_plush:gasha/dar_pareja
execute store result score #r emi_gasha run random value 1..1000
execute if score #r emi_gasha matches 1..400 run return run function emi_plush:gasha/dar_comun
execute if score #r emi_gasha matches 401..650 run return run function emi_plush:gasha/dar_bueno
execute if score #r emi_gasha matches 651..740 run return run function emi_plush:gasha/dar_epico
execute if score #r emi_gasha matches 741..840 run return run function emi_plush:gasha/dar_mod
execute if score #r emi_gasha matches 841..910 run return run function emi_plush:gasha/dar_peluche
execute if score #r emi_gasha matches 911..980 run return run function emi_plush:gasha/dar_pokemon
execute if score #r emi_gasha matches 981..1000 run return run function emi_plush:gasha/dar_pareja
