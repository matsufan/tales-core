by Carlos Chinchilla

http://twitter.com/calufa

http://linkedin.com/in/calufa

### About Tales
Tales is a block tolerant (IP blocking) web scraper that runs on top of aws and rackspace. Tales is design to be easy to deploy, configure, and manage. With Tales you can scrape 10s or even 100s of domains concurrently.

Tales is made in java, javascript/html and uses mysql, redis, and git.

Tales currently only runs on Ubuntu 10.04 Lucid -- Tales is calling shell scripts inside the app, this needs to be replaced by a "apache licensed version of sigar".

### Block tolerant

Tales is design to scrape the web continuously, even when the domain being scraped blocks the scraper server ip; it goes around this problem by fail-overing to a new node (server).

### Develop, deploy and build

Its very easy to code the scraper instructions, also called Templates. Once the templates are ready, all you need to do is push the code into git (git push origin), and the nodes alive will grab the code and recompile themselves.

You can also have several branches (git) with different configurations and templates -- environments. This gives you the ability of running tests in a separate set of servers.

### Management
Tales gives you a dashboard (javascript/html) where you can supervise the processes running on all the nodes -- Tales use websockets to stream the data from the processes to the dashboard.

In the dashboard you can also kill processes, delete servers, check solr, and look at critical errors.

There is a centralized log database that keeps logs of the activity and errors that happens on the system. The logging system, saves error information, server where the error occurred, and other useful data.

### Scrape, backup and shutdown

One of the ideas with Tales was that it should be able to grab data (scrape), back it up (backup), and then be able to shut down to minimize costs (shutdown).

If you want to continue scraping, you can simply create a new node, run the restore backup class, and start the scraper again.

Data is backup into AWS S3. The gzip file comes with a timestamp (Date.getTime()), the server ip that put the dump there, and the file name. Inside the gzip file there is a simple sql dump. The idea with the backups is that you could run map/reduce jobs on those sqls -- I will add support for AWS EMR soon.

You can also store the data into mongoDB and Solr; which comes prepack in all the nodes.

### Updates / Data states

Tales is design to keep updates of the data that you scrape. For instance, if a twitter user changes his location from "CR" to "SF", Tales will keep "CR" and store "SF"; keeps a log of the changes. 

This is very useful if you want to do regressions, some math, or see how data evolves.

### [How to install Tales](https://github.com/calufa/tales-core/wiki/Installing-Tales-on-Ubuntu-10.04-Lucid)

### [Tales workflow sample](https://github.com/calufa/tales-core/wiki/Tales-workflow-sample)

### [Template structure](https://github.com/calufa/tales-core/wiki/About-templates)

### [Pushing templates](https://github.com/calufa/tales-core/wiki/Pushing)

### [Config file](https://github.com/calufa/tales-core/wiki/Config-file)

### [Database design](https://github.com/calufa/tales-core/wiki/Database-design)

### [MongoDB and Solr](https://github.com/calufa/tales-core/wiki/MongoDB-and-Solr)

### [TalesDB updateAttribute vs addAttribute](https://github.com/calufa/tales-core/wiki/TalesDB-updateAttribute-vs-addAttribute)

### [tales.templates.TemplateCommon](https://github.com/calufa/tales-core/wiki/tales.templates.TemplateCommon)

### Tales Components

* [tales-core services](https://github.com/calufa/tales-core/wiki/Tales-Core-services)
* [tales-templates](https://github.com/calufa/tales-core/wiki/Tales-Templates)
* [tales-dashboard](https://github.com/calufa/tales-core/wiki/Tales-Dashboard)
* [tales-api](https://github.com/calufa/tales-core/wiki/Tales-API)
