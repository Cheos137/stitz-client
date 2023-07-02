# StiTz Client

This is a simple VoIP Client made for use with [StellwerkSim](https://stellwerksim.de/), written in java. This client uses the IAX2 protocol to communicate with one specific Asterisk server: `stitz.stellwerksim.de`. The IAX2 implementation is incomplete but sufficient for communication with the (quite outdated) remote Asterisk server and should work with modern versions too (while not supporting each and every feature thereis to offer).

An installer/auto-updater is currently being worked on, in the meantime refer to [manual installation](#manual-installation).

Pre-built versions will be available [here](https://github.com/Cheos137/stitz-client/releases).


## Building

This project uses the gradle wrapper toolchain. You can build the project by executing `gradlew build` (or `./gradlew build` respectively) in the project root directory. Please note that a working JDK17 (or newer) installation is required.

Once built, the client can be installed as follows:


### Automatic installer

Currently being worked on.


### Manual installation

The client itself can be found at `<project root>/stitz-client/build/libs/stitz-client-<version>.jar`. Though, due to limitations of CEF/JCEF, web ui files cannot be used from inside the jar. Usually, the installer would extract them for us, we however need to manually complete this step. Once you have picked the installation directory (and put the jarfile there), either open the jarfile using any commonly used .zip viewer and extract the `/static/` folder to the installation directory. The folder structure should look like follows: `<installation directory>/static/index.html` (with both a `.css` and `.js` file next to it). Alternatively you can also source the `/static/` folder from inside `<project root>/stitz-client/src/main/resources/`.

Once installed, the application can be run by executing `java -jar stitz-client-<version>.jar` (or `javaw -jar stitz-client-<version>.jar` respectively). Please note that this command **MUST** be executed from inside the installation directory as a different working directory will cause the client to install CEF to a wrong directory and to not find the previously extracted web ui files.
