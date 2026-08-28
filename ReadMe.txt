This file explains how to setup, build and run the Hotel Management System application

Before starting, ensure these are pre-installed:
- JDK, recommended versions will 17, 21, or 26
- Apache Ant installed and added to system PATH

1) run "seed.bat", type Y when prompted to confirm resetting the database
	- alternatively, you can just manually delete all the .dat files
	- then run "build.bat", and followed by "java -jar dist\HotelManagementSystem.jar --seed"

2) run "build.bat"

3) run "run.bat"


Alternative option:
- run "br.bat", it will quickly build up and run immediately, however sometimes it may pass although the codebase have java error as it doesn't clean the whole environment, so it's better to run "build.bat" instead

Of course, you may try running this in NetBeans IDE as well
However, the output may differ as it's IDE is not a real terminal
