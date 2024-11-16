import android.util.Log
import com.example.m3zebrascan.Item
import com.opencsv.CSVReader
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.InputStreamReader

class DocumentReader {

    fun readCsvFile(inputStream: InputStream): List<Item> {
        val items = mutableListOf<Item>()

        val reader = CSVReader(InputStreamReader(inputStream))
        var line: Array<String>?
        var lineNumber = 1 // Строки с данными начинаются со 2-й строки

        // Чтение строк CSV файла
        line = reader.readNext() // Пропускаем первую строку с заголовками
        while (reader.readNext().also { line = it } != null) {
            if (line != null) {
                val name = line!![0]
                val code = line!![1]
                val quantity = line!![2]
                val scanned = line!![3]

                if (name.isBlank()) {
                    throw IllegalArgumentException("В строке $lineNumber отсутствует название")
                }
                if (code.isBlank()) {
                    throw IllegalArgumentException("В строке $lineNumber отсутствует штрихкод")
                }
                if (quantity.isBlank()) {
                    throw IllegalArgumentException("В строке $lineNumber отсутствует количество")
                }

                lineNumber++
                val item = Item(name, code, quantity.toInt(), scanned.toInt(), 0)
                items.add(item)
            }
        }

        reader.close()
        return items
    }

    fun readXlsxFile(inputStream: InputStream): List<Item> {
        val items = mutableListOf<Item>()
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0) // Получаем первый лист
        val dataFormatter = DataFormatter() // Создаем экземпляр DataFormatter
        Log.d("ExcelSheetInfo",sheet.lastRowNum.toString())
        for (row in sheet) {
            // Пропускаем первую строку с заголовками
            if (row.rowNum == 0) continue

            val name = dataFormatter.formatCellValue(row.getCell(0))
            val code = dataFormatter.formatCellValue(row.getCell(1))
            val quantityString = dataFormatter.formatCellValue(row.getCell(2))
            val scannedString = dataFormatter.formatCellValue(row.getCell(3))

            if (name.isBlank()) {
                throw IllegalArgumentException("В строке ${row.rowNum + 1} отсутствует название")
            }
            if (code.isBlank()) {
                throw IllegalArgumentException("В строке ${row.rowNum + 1} отсутствует штрихкод")
            }
            if (quantityString.isBlank()) {
                throw IllegalArgumentException("В строке ${row.rowNum + 1} отсутствует количество")
            }

            val quantity = quantityString.toIntOrNull() ?: throw IllegalArgumentException("В строке ${row.rowNum + 1} количество должно быть числом")
            val scanned = scannedString.toIntOrNull() ?: 0
            val item = Item(name, code, quantity, scanned, 0)
            items.add(item)
        }

        workbook.close()
        return items
    }
}