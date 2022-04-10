package erroneous

import (
	nonExist "127.0.0.1/non-exist"
	"fmt"
	"rsc.io/quote"
)

func PrintHello() {
	fmt.Println(quote.Hello())
	nonExist.hello()
}
