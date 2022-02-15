package aia.persistence.serialization

import akka.serialization.jackson.JacksonObjectMapperFactory
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class KotlinModuleJacksonObjectMapperFactory : JacksonObjectMapperFactory() {
    override fun newObjectMapper(bindingName: String, jsonFactory: JsonFactory): ObjectMapper {
        return jacksonObjectMapper().registerKotlinModule()
    }
}