#include "water_pump.h"
#include <avr/io.h>
#include <stdbool.h>

#define WATER_PUMP_PORT PORTC
#define WATER_PUMP_DDR  DDRC
#define WATER_PUMP_PIN  PC7

static uint16_t pump_timer = 0;
static bool timer_active = false;

void water_pump_init(void) {
    WATER_PUMP_DDR |= (1 << WATER_PUMP_PIN);
    
    water_pump_stop();
}

void water_pump_start(void) {
    WATER_PUMP_PORT |= (1 << WATER_PUMP_PIN);
}

void water_pump_stop(void) {
    WATER_PUMP_PORT &= ~(1 << WATER_PUMP_PIN);
    
    timer_active = false;
    pump_timer = 0;
}

bool water_pump_is_running(void) {
    return (WATER_PUMP_PORT & (1 << WATER_PUMP_PIN)) != 0;
}

void water_pump_run_for(uint16_t seconds) {
    water_pump_start();
    
    pump_timer = seconds;
    timer_active = true;
}

void water_pump_update(void) {
    if (timer_active && pump_timer > 0) {
        pump_timer--;
        
        if (pump_timer == 0) {
            water_pump_stop();
            timer_active = false;
        }
    }
}
