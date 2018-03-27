package net.corda.behave.scenarios

import net.corda.behave.scenarios.api.StepsBlock
import net.corda.behave.scenarios.api.StepsProvider
import org.junit.Test
import org.reflections.Reflections
import kotlin.test.assertEquals

class StepsProviderTests {

    @Test
    fun `module can discover steps providers`() {
        val reflections = Reflections("net.corda.behave")

        val foundProviders = mutableListOf<String>()
        reflections.getSubTypesOf(StepsProvider::class.java).forEach {
            foundProviders.add(it.simpleName)
            val instance = it.newInstance()
            val name = instance.name
            assert(it.simpleName.contains(name))
        }

        assertEquals(2, foundProviders.size)
        assert(foundProviders.contains("FooStepsProvider"))
        assert(foundProviders.contains("BarStepsProvider"))
    }

    class FooStepsProvider : StepsProvider {

        override val name: String
            get() = "Foo"

        override val stepsDefinition: StepsBlock
            get() = DummyStepsBlock()
    }

    class DummyStepsBlock : StepsBlock(ScenarioState()) {
        override fun initialize() {
        }
    }
}