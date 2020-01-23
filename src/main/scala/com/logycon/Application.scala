package com.logycon

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

object Application extends App {
  SpringApplication.run(classOf[SpringBootConfig])
}

@SpringBootApplication
class SpringBootConfig
