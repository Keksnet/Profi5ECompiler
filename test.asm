0300:
@wait
03cc:
@error
8000:
mvi a,82
out 03   ; toggle all leds off
@reset
mvi a,01 ; the the initial value
jmp @error ; jump to the error routine
@loop
out 00   ; toggle the leds
mov b,a
mvi a,01  ; set 1/10 wait time
call @wait ; wait
mov a,b
mvi c,80 ; the value for comparing
cmp c
jz @reset ; jump if a is greater than c
add a ; duplicate a to go up in binary
jmp @loop ; otherwise jump back to the loop beginning
