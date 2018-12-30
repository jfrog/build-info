class DepsSanity {
    static {
        println "Checking dependencies..."
        Logger.getLogger("DepsSanity");
        com.mysql.jdbc.Driver.newInstance()
    }
}
