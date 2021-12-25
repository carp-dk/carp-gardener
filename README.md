![GARDENER-LOGO](docs/gardener_logo.png)
# Gardener Package

Gardener is an open-source application for collection of wearable activity
and health data from web-based services. Custom data transformation registration
is supported to convert the data into a desired format. 
The package consists of two parts: 

- [**gardener.core**](docs/core_readme.md): 
Gardener Core is a general-purpose framework that defines
the main logic of data collection and transformation.
It can be integrated into existing projects or into its own service by providing
the required dependencies, which are described in its 
[README](docs/core_readme.md) file.


- [**gardener.carp-implementation**](docs/impl_readme.md): 
Gardener CARP Implementation provides an implementation of the framework for
[CACHET](https://www.cachet.dk/).

The following APIs are currently supported:
- [Fitbit](https://dev.fitbit.com/)
- [Garmin](https://developer.garmin.com/)
- [Withings](https://oauth.withings.com/)
- [Dexcom](https://developer.dexcom.com/)
