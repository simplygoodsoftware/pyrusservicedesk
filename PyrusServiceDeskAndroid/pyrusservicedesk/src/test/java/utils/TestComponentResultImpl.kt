package utils

internal class TestComponentResultImpl<Model, Effect>: TestComponentResult<Model, Effect> {
    override val models = ArrayList<Model>()
    override val effects = ArrayList<Effect>()

    private var modelIndex = 0
    private var effectIndex = 0

    override fun currentModel(): Model = models[modelIndex]

    override fun nextModel(): Model = models[++modelIndex]

    override fun currentEffect(): Effect = effects[effectIndex]

    override fun nextEffect(): Effect = effects[++effectIndex]
}