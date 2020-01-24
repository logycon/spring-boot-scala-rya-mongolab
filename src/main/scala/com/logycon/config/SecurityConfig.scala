package com.logycon.config

import java.util

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.{EnableWebSecurity, WebSecurityConfigurerAdapter}
import org.springframework.web.cors.{CorsConfiguration, CorsConfigurationSource, UrlBasedCorsConfigurationSource}

@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter {

  override def configure(http: HttpSecurity): Unit = {
    http.cors().and().csrf().disable()
      .authorizeRequests()
      .antMatchers("/**").permitAll()
  }

  @Bean
  def corsConfigurationSource: CorsConfigurationSource = {
    val configuration = new CorsConfiguration
    configuration.setAllowedOrigins(util.Arrays.asList("*"))
    configuration.setAllowedMethods(util.Arrays.asList("GET", "POST", "PUT", "DELETE"))
    configuration.setAllowCredentials(true)
    val source = new UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    source
  }

}
