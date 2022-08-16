mvi a,82
out 03
mvi a,00
@loop
out 00
mov b,a
mvi a,05
call 0300
mov a,b
inr a
jmp @loop