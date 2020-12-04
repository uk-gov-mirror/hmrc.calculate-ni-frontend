
# calculate-ni-frontend

Calculate NI is a frontend-only internal facing national insurance
contribution calculator. It consumes no API's and requires no backend
microservice. 

Technically the service could be entirely deployed as
static content were it not for the stride authentication. 

It eschews the normal 'one thing per page' design to provide an
interface tailored to allow faster operation for expert users. As such
it makes heavy use of javascript and once the page is loaded can
operate without any round-trips between the user-agent and the
microservice. 

No user data is transmitted between the browser and microservice apart
from the normal requests for resources inherent in browsing, the
application has no 'state' to retain. Additionally the logic behind
the calculations and the historical rates and bands are entirely
within the public domain. As such the service neither gathers nor is
able to leak any data that could be considered sensitive. 

## Supported calculations



## Configuration

This service replaced an access database that required each new
financial year to be coded anew. Instead it employs a configuration
file that defines all the tax rules for a financial year. Adding in a
new financial year only requires adding a new entry into this file and
redeploying. 

The configuration file is in [HOCON format]("https://github.com/lightbend/config/blob/master/HOCON.md#hocon-human-optimized-config-object-notation")
for ease of editing, however as HOCON cannot be easily parsed by
javascript this is converted automatically by an SBT task into a plain
JSON file for parsing by the frontend and is injected into the react
application.

## Build process 

```
 +-----------------------+
 | installReactDeps      +-----------------------+
 +-----------------------+                       |
 +-----------------------+            +----------v------------+             +----------------------+
 | copyInJS              +------------> buildReactApp         +-------------> play                 |
 +-----------------------+            +----------^------------+             +----------------------+
 +-----------------------+                       |
 | convertConfig         +-----------------------+
 +-----------------------+
```

calculate-ni-frontend is technically a multi-project build, however as
the MDTP build pipeline expects a single microservice artifact per
github repo all of them apart from the play application have had their
package tasks suppressed. 

The frontend itself is created with react. You will need npm installed
in order to build the microservice.

Provided you have npm installed, calling `sbt run` should call all the
dependent tasks and launch the application as expected. However while
working on the frontend it may be easier to do development directly
within the react application. 

`installReactDeps` calls `npm install` within the react directory to load
any JS dependencies. 

`copyInJS` builds the calculation logic as JS from the scala code via
scalaJS. 

`convertConfig` translates the HOCON file to JSON format for consumption by
the react application. 

`buildReactApp` builds the react component via npm and places the
resulting js and static assets inside the play application. 

## Limitations 

- The lack of support for multi-project builds on the build pipeline
  means that we cannot meaningfully separate out the 'common'
  subproject containing the calculation logic to make it available
  elsewhere, though this could always be exposed as a microservice. 
  
- We're running an old version of scalajs (0.6.33) due to a binary
  incompatibility problem with the sbt-artifactory plugin used in the
  build pipeline (BDOG-1179). Once this issue has been resolved it is
  advisable to upgrade to the latest scalajs.

- npm can take a while to build the react app. 

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
