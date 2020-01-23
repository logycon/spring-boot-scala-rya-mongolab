package com.logycon.config

import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.{Components, OpenAPI}
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class OpenApiConfig {

  @Bean
  def customOpenAPI: OpenAPI = {
    new OpenAPI().components(new Components)
      .info(new Info()
        .title("Api title")
        .description("Api description"))
  }
}
