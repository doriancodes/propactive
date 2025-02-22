package io.github.propactive.property

import io.github.propactive.config.BLANK_PROPERTY
import io.github.propactive.config.UNSPECIFIED_ENVIRONMENT
import io.github.propactive.property.PropertyFailureReason.PROPERTY_FIELD_HAS_INVALID_TYPE
import io.github.propactive.property.PropertyFailureReason.PROPERTY_FIELD_INACCESSIBLE
import io.github.propactive.property.PropertyFailureReason.PROPERTY_SET_MANDATORY_IS_BLANK
import io.github.propactive.property.PropertyFailureReason.PROPERTY_VALUE_HAS_INVALID_TYPE
import io.github.propactive.type.INTEGER
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
internal class PropertyFactoryTest {
    @Nested
    inner class HappyPath {
        @Test
        fun `given an empty object, when factory creates a DAO, then it should return an empty list`() {
            PropertyFactory
                .create(Empty::class.members)
                .shouldBeEmpty()
        }

        @Test
        fun `given a PropertyWithNoEnvironmentKey, when factory creates a DAO, then it should associate that property with an UNSPECIFIED_ENVIRONMENT`() {
            PropertyFactory
                .create(PropertyWithUnspecifiedEnvironmentKey::class.members)
                .apply {
                    this shouldHaveSize 1
                    this.first().environment shouldBe UNSPECIFIED_ENVIRONMENT
                    this.first().name shouldBe "test.resource.value"
                    this.first().value shouldBe "value"
                }
        }

        @Test
        fun `given a PropertyWithSingleEnvironmentKey, when factory creates a DAO, then it should associate that property with correct environment`() {
            PropertyFactory
                .create(PropertyWithSingleEnvironmentKey::class.members)
                .apply {
                    this shouldHaveSize 1
                    this.first().environment shouldBe "env0"
                    this.first().name shouldBe "test.resource.value"
                    this.first().value shouldBe "value"
                }
        }

        @Test
        fun `given a PropertyWithEnvironmentKeyExpansion, when factory creates a DAO, then it should expand that property values to multiple environments`() {
            PropertyFactory
                .create(PropertyWithEnvironmentKeyExpansion::class.members)
                .apply {
                    this shouldHaveSize 2
                    this.forEachIndexed { index, model ->
                        model.environment shouldBe "env$index"
                        model.value shouldBe "value"
                    }
                }
        }

        @Test
        fun `given a PropertyWithMultipleEnvironmentKeysWithDifferentValues, when factory creates a DAO, then it should expand that property to multiple environments with correct value mapping`() {
            PropertyFactory
                .create(PropertyWithMultipleEnvironmentKeysWithDifferentValues::class.members)
                .apply {
                    this shouldHaveSize 3
                    this.forEachIndexed { index, model ->
                        model.environment shouldBe "env$index"
                        model.value shouldBe "$index"
                    }
                }
        }
    }

    @Nested
    inner class SadPath {
        @Test
        fun `given an object with a PrivateProperty, when factory creates a DAO, then it should error`() {
            assertThrows<IllegalStateException> {
                PropertyFactory
                    .create(PrivateProperty::class.members)
            }.message shouldBe PROPERTY_FIELD_INACCESSIBLE("PRIVATE")()
        }

        @Test
        fun `given an object with an IncorrectFieldType, when factory creates a DAO, then it should error`() {
            assertThrows<IllegalStateException> {
                PropertyFactory
                    .create(IncorrectFieldType::class.members)
            }.message shouldBe PROPERTY_FIELD_HAS_INVALID_TYPE("NOT_A_STRING")()
        }

        @Test
        fun `given an object with an InvalidPropertyValueType, when factory creates a DAO, then it should error`() {
            assertThrows<IllegalArgumentException> {
                PropertyFactory
                    .create(InvalidPropertyValueType::class.members)
            }.message shouldBe PROPERTY_VALUE_HAS_INVALID_TYPE("test.resource.value", "test", "not an int", INTEGER)()
        }

        @Test
        fun `given an object with MandatoryPropertyWithBlankValue, when factory creates a DAO, then it should error`() {
            assertThrows<IllegalArgumentException> {
                PropertyFactory
                    .create(MandatoryPropertyWithBlankValue::class.members)
            }.message shouldBe PROPERTY_SET_MANDATORY_IS_BLANK("test.resource.value", "")()
        }

        @Test
        fun `given an object with NonMandatoryPropertyWithBlankValue, when factory creates a DAO, then it should not error`() {
            assertDoesNotThrow {
                PropertyFactory
                    .create(NonMandatoryPropertyWithBlankValue::class.members)
                    .first().apply {
                        name shouldBe NonMandatoryPropertyWithBlankValue.PROPERTY_NAME
                        value shouldBe BLANK_PROPERTY
                        environment shouldBe UNSPECIFIED_ENVIRONMENT
                    }
            }
        }
    }

    // HAPPY PATH OBJECTS

    object Empty

    object PropertyWithUnspecifiedEnvironmentKey {
        @Property(["value"])
        const val UNSPECIFIED_ENVIRONMENT_PROPERTY = "test.resource.value"
    }

    object PropertyWithSingleEnvironmentKey {
        @Property(["env0:value"])
        const val SINGULAR_ENVIRONMENT_PROPERTY = "test.resource.value"
    }

    object PropertyWithEnvironmentKeyExpansion {
        @Property(["env0/env1:value"])
        const val MULTIPLE_ENVIRONMENT_PROPERTY = "test.resource.value"
    }

    object PropertyWithMultipleEnvironmentKeysWithDifferentValues {
        @Property(
            value = ["env0: 0", "env1: 1", "env2: 2"],
            type = INTEGER::class,
        )
        const val MULTIPLE_ENVIRONMENT_PROPERTY = "test.resource.value"
    }

    // SAD PATH OBJECTS

    object PrivateProperty {
        @Property([":cannot access field getter"])
        private const val PRIVATE = "test.resource.value"
    }

    object IncorrectFieldType {
        @Property([":field is not a String"])
        const val NOT_A_STRING = 10101
    }

    object InvalidPropertyValueType {
        @Property(["test: not an int"], type = INTEGER::class)
        const val PROPERTY_NAME = "test.resource.value"
    }

    object MandatoryPropertyWithBlankValue {
        @Property([":"], mandatory = true)
        const val PROPERTY_NAME = "test.resource.value"
    }

    object NonMandatoryPropertyWithBlankValue {
        @Property(mandatory = false)
        const val PROPERTY_NAME = "test.resource.value"
    }

}