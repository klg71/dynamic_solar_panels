# dynamic_solar_panels
Activate or deactivate solar panels according to luminosity.

Panels are organized into strings of parallel modules.
Each of them but the first having a smart switch/relais to turn them off or on.

I use the Shell Plus 1 and the TUYA 16A Mini Smart Switch.

I measure the current on each string and enable more modules if it drops too low or deactivate them if it is too high.

I am using the Astro-E Inverter 1600 4T with the DTU 003833169814.

The modbus protocol it uses is incomplete and had to be reverse engineered.
