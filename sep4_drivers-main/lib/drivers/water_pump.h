#ifndef WATER_PUMP_H_
#define WATER_PUMP_H_

#include <avr/io.h>
#include <stdbool.h>

/**
 * @brief Initialize the water pump control on the specified port
 *
 * The water pump requires a 12V external power supply and is controlled
 * via a Relay PCB connected to the Arduino. This function sets up the
 * control pin as an output.
 *
 * Default configuration uses PC7 as in the documentation.
 */
void water_pump_init(void);

/**
 * @brief Start the water pump
 *
 * Sets the control pin high to activate the relay and start the pump.
 */
void water_pump_start(void);

/**
 * @brief Stop the water pump
 *
 * Sets the control pin low to deactivate the relay and stop the pump.
 */
void water_pump_stop(void);

/**
 * @brief Get the current state of the water pump
 *
 * @return true if the pump is running, false if the pump is stopped
 */
bool water_pump_is_running(void);

/**
 * @brief Run the water pump for a specified duration
 *
 * @param seconds The duration in seconds to run the pump
 *
 * This function is non-blocking and should be used together with
 * water_pump_update() in the main loop to actually control the timing.
 */
void water_pump_run_for(uint16_t seconds);

/**
 * @brief Update the water pump timer
 *
 * This function should be called regularly (e.g., once per second) to
 * update the water pump timer if water_pump_run_for() is used.
 */
void water_pump_update(void);

#endif /* WATER_PUMP_H_ */
