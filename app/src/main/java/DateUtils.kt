import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

object DateUtils {
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}