AK DGGS Photo Database
======================

This is the source code for the [Alaska](http://alaska.gov)
[Division of Geological and Geophysical Survey](http://dggs.alaska.gov)'s
photo database. An in-production version of this database can be viewed at
[http://maps.dggs.alaska.gov/photodb/](http://maps.dggs.alaska.gov/photodb/).

PhotoDB was developed using Java 8 and
[Apache Tomcat](https://tomcat.apache.org/) 8.
It uses [PostgreSQL](https://www.postgresql.org/) as it's data store
and [Apache Solr](https://lucene.apache.org/solr/) for searching.

PhotoDB also uses
[MyBatis](http://www.mybatis.org/mybatis-3/),
[Apache Commons FileUpload](https://commons.apache.org/proper/commons-fileupload/),
[M's JSON](http://mjson.sourceforge.net/),
[ImgScalr](https://github.com/rkalla/imgscalr), and
[Metadata-Extractor](https://drewnoakes.com/code/exif/). These libraries
are included in this repository for convenience.


Compiling
---------

You will need a relatively modern version of Java JDK installed, as well as
[Apache Tomcat](https://tomcat.apache.org/) and
[Apache Ant](https://ant.apache.org/). This application has been tested
with Tomcat 7 and Tomcat 8, but newer versions and different
servlet containers will likely work as well.

Edit `src/WEB-INF/web.xml` to set important runtime variables for your
installation (e.g. the URL to your Solr instance, your root installation
URL, and your USGS Science Data Services Collection ID.)

Edit `build.xml` to indicate the directory you have Tomcat installed in.

Finally, run `ant war` to generate a web application resource (WAR) file.


Installation
------------

Create a [PostgreSQL](https://www.postgresql.org/) database and add the
[PostGIS](http://postgis.net/) extension. In your Tomcat configuration,
add a JDBC resource pointing to your newly configured database named
`jdbc/photodb`.

Configure your [Apache Solr](https://lucene.apache.org/solr/) instance -
example configuration can be found in the `solr-example` directory.

Finally, copy `photodb.war` to servlet container's webapps directory.
