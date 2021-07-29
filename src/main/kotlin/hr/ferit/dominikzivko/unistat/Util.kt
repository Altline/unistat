package hr.ferit.dominikzivko.unistat

import com.gargoylesoftware.htmlunit.html.HtmlPage
import hr.ferit.dominikzivko.unistat.gui.floatToString
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.chart.PieChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.util.Duration
import java.nio.file.Path
import java.time.LocalDate

val workDir: Path get() = Path.of(System.getProperty("app.workdir"))

fun LocalDate.isToday() = this == LocalDate.now()
fun LocalDate.isYesterday() = this == LocalDate.now().minusDays(1)
fun LocalDate.isPastWeek() = this in LocalDate.now().minusWeeks(1)..LocalDate.now()
fun LocalDate.isPastMonth() = this in LocalDate.now().minusMonths(1)..LocalDate.now()

val HtmlPage.urlString: String get() = url.toExternalForm()


fun boundLabelFor(vararg dependencies: Observable, func: () -> String?) = Label().apply {
    bindText(*dependencies, func = func)
}

fun Label.bindText(vararg dependencies: Observable, func: () -> String?) {
    textProperty().bind(Bindings.createStringBinding(func, *dependencies))
}

fun PieChart.bindData(vararg dependencies: Observable, func: (MutableList<PieChart.Data>) -> Unit) {
    dataProperty().bind(makeChartBinding(dependencies, func))
}

fun <X, Y> XYChart<X, Y>.bindData(vararg dependencies: Observable, func: (MutableList<XYChart.Series<X, Y>>) -> Unit) {
    dataProperty().bind(makeChartBinding(dependencies, func))
}

private fun <T> makeChartBinding(
    dependencies: Array<out Observable>,
    func: (MutableList<T>) -> Unit
): ObservableValue<ObservableList<T>> {
    return Bindings.createObjectBinding({
        val newData = FXCollections.observableArrayList<T>()
        func(newData)
        return@createObjectBinding newData
    }, *dependencies)
}

/**
 * Adds a data change listener that installs tooltips on pie slices for all new data that is set after this call.
 *
 * **Note:** This should be called before binding the data property to ensure tooltips are installed on the first dataset.
 */
fun PieChart.enablePieTooltips(showDelay: Duration = Duration.seconds(0.5), isValueInteger: Boolean = false) {
    dataProperty().addListener { _, _, newValue ->
        newValue.forEach { item ->
            val yString = if (isValueInteger) item.pieValue.toInt() else floatToString(item.pieValue)
            val tooltip = Tooltip(item.name + "\n" + yString).apply {
                this.showDelay = showDelay
            }
            Tooltip.install(item.node, tooltip)
        }
    }
}

fun <X, Y : Number> XYChart<X, Y>.enableBarTooltips(showDelay: Duration = Duration.seconds(0.5)) {
    dataProperty().addListener { _, _, newValue ->
        newValue.forEach { series ->
            series.data.forEach { item ->
                val yString = when (item.yValue) {
                    is Float, is Double -> floatToString(item.yValue)
                    else -> item.yValue.toString()
                }
                val tooltip = Tooltip(item.xValue.toString() + "\n" + yString).apply {
                    this.showDelay = showDelay
                }
                Tooltip.install(item.node, tooltip)
            }
        }
    }
}