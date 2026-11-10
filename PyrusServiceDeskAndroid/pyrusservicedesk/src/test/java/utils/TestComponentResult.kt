package utils

interface TestComponentResult<Model, Effect> {
    val models: List<Model>
    val effects: List<Effect>

    fun currentModel(): Model
    fun nextModel(): Model
    fun currentEffect(): Effect
    fun nextEffect(): Effect
}