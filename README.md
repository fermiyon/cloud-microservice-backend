# Cloud Microservice on Heroku #

## Build & Run ##

```sh
$ cd cloud-microservice-backend
$ sbt
> run
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

# Heroku #

## Deploy to Heroku ##

```sh
$ cd cloud-microservice-backend
$ heroku create
$ git push heroku master
```
