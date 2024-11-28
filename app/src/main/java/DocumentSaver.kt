import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import com.example.m3zebrascan.Item
import com.example.m3zebrascan.Inventory.InventoryItem

class DocumentSaver(private val context: Context) {

    fun saveInventory(uri: Uri, items: List<InventoryItem>) {
        saveItemsToXlsx(
            uri = uri,
            items = items,
            headers = listOf("Штрих-код", "Количество")
        ) { row, item ->
            row.createCell(0).setCellValue(item.code)
            row.createCell(1).setCellValue(item.quantity.toDouble())
        }
    }

    private fun <T> saveItemsToXlsx(
        uri: Uri,
        items: List<T>,
        headers: List<String>,
        populateRow: (Row, T) -> Unit
    ) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Лист 1"))

                // Создаем стиль для заголовков
                val headerCellStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                    fillPattern = CellStyle.SOLID_FOREGROUND
                }

                // Создаем строку заголовков
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerCellStyle
                }

                // Заполняем данные
                items.forEachIndexed { index, item ->
                    val row: Row = sheet.createRow(index + 1)
                    populateRow(row, item)
                }

                // Сохраняем workbook
                workbook.write(outputStream)
                workbook.close()
            }
        } catch (e: IOException) {
            throw e
        }
    }
}