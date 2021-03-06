package hr.ferit.dominikzivko.unistat.gui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.AppBase
import hr.ferit.dominikzivko.unistat.bindData
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.enableBarTooltips
import hr.ferit.dominikzivko.unistat.gui.component.ArticleView
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.chart.BarChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.StackPane
import javafx.util.Duration
import org.koin.core.context.GlobalContext
import kotlin.math.max

class GuiArticleStats {
    private val app: AppBase by lazy { GlobalContext.get().get() }

    @FXML
    private lateinit var articleChartHolder: ScrollPane

    @FXML
    private lateinit var spendingByArticleChart: StackedBarChart<String, Number>

    @FXML
    private lateinit var amountByArticleChart: BarChart<String, Number>


    @FXML
    private lateinit var articlesTable: TableView<Article>

    @FXML
    private lateinit var colName: TableColumn<Article, String>

    @FXML
    private lateinit var colPrice: TableColumn<Article, Number>

    @FXML
    private lateinit var colAmount: TableColumn<Article, Number>


    @FXML
    private lateinit var detailsPanel: StackPane

    @FXML
    private lateinit var lblNoArticle: Label

    private val articleView = ArticleView()

    private val billsByArticleProperty = SimpleMapProperty<Article, List<Bill>>()
    private val billsByArticle: ObservableMap<Article, List<Bill>> by billsByArticleProperty

    private val entriesByArticleProperty = SimpleMapProperty<Article, List<BillEntry>>()
    private val entriesByArticle: ObservableMap<Article, List<BillEntry>> by entriesByArticleProperty

    private val sortedEntriesByArticleProperty = SimpleListProperty<Pair<Article, List<BillEntry>>>()
    private val sortedEntriesByArticle: ObservableList<Pair<Article, List<BillEntry>>> by sortedEntriesByArticleProperty

    private val selectedArticleProperty get() = articlesTable.selectionModel.selectedItemProperty()
    private val selectedArticle get() = selectedArticleProperty.value

    @FXML
    private fun initialize() {
        setupBillBindings()
        setupAmountByArticleChart()
        setupSpendingByArticleChart()
        setupArticlesTable()
        setupDetailsPanel()
        selectedArticleProperty.addListener { _, oldValue, newValue ->
            spendingByArticleChart.updateSelection(oldValue, newValue)
            amountByArticleChart.updateSelection(oldValue, newValue)
        }
    }

    private fun setupBillBindings() {
        billsByArticleProperty.bind(Bindings.createObjectBinding({
            FXCollections.observableMap(
                app.repository.articles.associateWith { article ->
                    app.repository.filteredBills.filter { it.articles.contains(article) }
                })
        }, app.repository.filteredBills))

        entriesByArticleProperty.bind(Bindings.createObjectBinding({
            FXCollections.observableMap(
                billsByArticle.mapValues { (article, bills) ->
                    bills.map { bill ->
                        bill.entries.find { it.article == article }!!
                    }
                })
        }, billsByArticleProperty))

        sortedEntriesByArticleProperty.bind(Bindings.createObjectBinding({
            FXCollections.observableList(
                entriesByArticle.toList().sortedByDescending { it.second.totalValue }
            )
        }, entriesByArticleProperty))
    }

    private fun setupSpendingByArticleChart() {
        spendingByArticleChart.apply {
            addClickSelection()
            enableBarTooltips(Duration.ZERO)
            bindData(sortedEntriesByArticleProperty) { series ->
                val costData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
                val subsidyData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

                sortedEntriesByArticle.forEach { (article, entries) ->
                    costData += XYChart.Data(article.name, entries.totalCost)
                    subsidyData += XYChart.Data(article.name, entries.totalSubsidy)
                }

                series += XYChart.Series(strings["chart_series_personalCost"], costData)
                series += XYChart.Series(strings["chart_series_subsidy"], subsidyData)
            }
            prefWidthProperty().bind(
                Bindings.createDoubleBinding({
                    max(articleChartHolder.width, entriesByArticle.size * 10.0)
                }, articleChartHolder.widthProperty())
            )
        }
    }

    private fun setupAmountByArticleChart() {
        amountByArticleChart.apply {
            addClickSelection()
            enableBarTooltips(Duration.ZERO)
            bindData(sortedEntriesByArticleProperty) { series ->
                val amountData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()

                sortedEntriesByArticle.forEach { (article, entries) ->
                    amountData += XYChart.Data(article.name, entries.totalAmount)
                }

                series += XYChart.Series(strings["chart_series_amountBought"], amountData)
            }
        }
    }

    private fun setupArticlesTable() {
        colName.setCellValueFactory { Bindings.createStringBinding({ it.value.name }) }
        colPrice.setCellValueFactory { Bindings.createFloatBinding({ it.value.fPrice }) }
        colAmount.setCellValueFactory { cellData ->
            Bindings.createIntegerBinding({
                entriesByArticle[cellData.value]?.totalAmount
            })
        }

        colPrice.setStringCellFactory { it.formatted }

        val sortedArticles = SortedList(app.repository.articles)
        sortedArticles.comparatorProperty().bind(articlesTable.comparatorProperty())
        articlesTable.items = sortedArticles
        articlesTable.sortOrder.setAll(colAmount, colName)
    }

    private fun setupDetailsPanel() {
        lblNoArticle.visibleProperty().bind(selectedArticleProperty.isNull)
        articleView.visibleProperty().bind(selectedArticleProperty.isNotNull)

        articleView.dataProperty.bind(Bindings.createObjectBinding({
            selectedArticle?.let {
                ArticleView.Data(it, billsByArticle[it] ?: emptyList())
            }
        }, selectedArticleProperty))

        detailsPanel.children += articleView
    }

    private fun selectArticle(articleName: String) {
        val article = articlesTable.items.find { it.name == articleName }
        articlesTable.selectionModel.select(article)
    }

    private fun XYChart<String, Number>.addClickSelection() {
        dataProperty().addListener { _, _, newValue ->
            newValue.forEach { series ->
                series.data.forEach { item ->
                    item.node.setOnMouseClicked { selectArticle(item.xValue) }
                }
            }
        }
    }

    private fun XYChart<String, Number>.updateSelection(oldValue: Article?, newValue: Article?) {
        data.forEach { series ->
            series.data.forEach { item ->
                if (item.xValue == oldValue?.name)
                    item.node.styleClass -= "chart-bar-selected"

                if (item.xValue == newValue?.name)
                    item.node.styleClass += "chart-bar-selected"
            }
        }
    }
}