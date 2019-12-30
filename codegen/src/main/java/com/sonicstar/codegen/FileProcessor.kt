package com.sonicstar.codegen

import com.google.auto.service.AutoService
import com.sonicstar.annotation.Singleton
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Created by cbutani on 12/30/2019.
 * Copyright (c) 2019 ANGUS SYSTEMS
 **/

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
class FileProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Singleton::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {

        roundEnvironment?.getElementsAnnotatedWith(Singleton::class.java)
            ?.forEach {
                val name = it.simpleName.toString()

                val packageName =
                    processingEnv
                        .elementUtils
                        .getPackageOf(it)
                        .toString()

                val fileName = name + "Singleton"

                val className = ClassName(packageName,name)

                val instance = PropertySpec
                    .builder(
                        "instance", className.topLevelClassName().copy(nullable = true))
                    .mutable()
                    .addModifiers(KModifier.PRIVATE, KModifier.FINAL)
                    .initializer("null").build()

                val getInstance =
                    FunSpec
                        .builder("getInstance")
                        .returns(className)
                        .addStatement("if (instance == null) { instance = %T() }", className.topLevelClassName())
                        .addStatement("return instance!!")
                        .build()

                val companion =
                    TypeSpec
                        .companionObjectBuilder()
                        .addProperty(instance)
                        .addFunction(getInstance)

                val builder =
                    TypeSpec
                        .classBuilder(fileName)
                        .addModifiers(KModifier.PUBLIC)
                        .addType(companion.build())
                        .build()


                val file =
                    FileSpec.builder(packageName, fileName).addType(builder).build()

                val kotlinGenerated = processingEnv.options[KOTLIN_GENERATED_NAME]
                file.writeTo(File(kotlinGenerated, "$fileName.kt"))

            }
        return true
    }

    companion object {
        const val KOTLIN_GENERATED_NAME = "kapt.kotlin.generated"
    }
}