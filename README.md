# hnfetchjs

## Summmary

This is a project to demo several libraries at once:

* 47Deg's [Fetch](https://github.com/47deg/fetch) library to grab Hacker News stories
* [Udash](http://udash.io/) to build an interative website frontend entirely in Scala
* [Reftree](https://github.com/stanch/reftree) to visualize data structures graphically

View the project live
- [http://heyes-jones.com/hnfetch/index.html](http://heyes-jones.com/hnfetch/index.html) 

Fetch is a Scala library inspired by [Haxl](https://github.com/facebook/haxl) which makes it easier to work with data sources that have latency (DB's, web services etc). In this example I use the Hacker News API as a data source. Once a Fetch is executed `Fetch Stories` you can click the `Last Fetch` tab to view a visualization of the Fetch operation. This shows a list of "rounds", how long they took and how many items were fetched. If you try to get the same stories again you will see that the Fetch diagram is simply Nil, because the items were already cached. You can clear the cache and run the fetch again.

Goals of this project

* Learn more about the Fetch library. Writing a custom cache and porting JVM code to Scala.js
* Provide a sandbox to demonstrate the Fetch using Reftree to visualize the rounds

This project is not

* A useful Hacker News client, although it could easily be extended to be one

## Installation

After cloning the repository and installing sbt you can build the frontend files and serve them from a local server using

```
sbt
~fastOptJS
```

You can now edit the Scala files to make changes and view the site at [http://localhost:12345/target/UdashStatic/WebContent/index.html](http://localhost:12345/target/UdashStatic/WebContent/index.html)

To publish the final pages

```sbt
set isSnapshot := false
compileStatics
```

The final output files will be located in the folder `hnfetchjs/target/UdashStatic/WebContent/`

[comment]: # (Start Copyright)
# Copyright

hnfetchjs is developed by Justin Heyes-Jones

Copyright (C) 2017 Justin Heyes-Jones

[comment]: # (End Copyright)






