import org.apache.log4j.*

class DepsSanity {
    static {
        println "Checking dependencies..."
        Logger.getLogger("DepsSanity");
        com.mysql.jdbc.Driver.newInstance()
    }
}
