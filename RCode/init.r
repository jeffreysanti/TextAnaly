library(rJava)

#####################################################################
#
#    IT IS RECOMENDED NOT TO MODIFY THIS FILE
#    AS IT LINKS TO JAVA CODE, AND CAN EASILY
#    BREAK
#    YOU MAY SAFELY MODIFY RCode/inc.r TO ADD 
#    YOUR OWNMETHODS OR MODIFY ONES INCLUDED.
#
#####################################################################

.jinit("dist/text_analy4.jar")
taContext__ <- .jnew("net/jeffreysanti/text_analy4/RInterface")
.jcall(taContext__, "V", "initR")

h <- function(P = NULL){
	if(is.null(P)) {
		.jcall(taContext__, "V", "helpMenu")
	}else{
		.jcall(taContext__, "V", "chHelp",P)
	}
}

g <- function(){
	.jcall(taContext__, "V", "showGui")
}

taLoad <- function(flPath){
	c <- .jnew("net/jeffreysanti/text_analy4/RDBContext", flPath)
	okay <- .jcall(c, "Z", "isOkay")
	if(!okay){
		print("Error Has Occured Loading DB")
		return(NULL)
	}
	return(c)
}

taProcessQuery <- function(context, numr){
	if(numr <= 0){
		return(matrix(numeric(0), 0,0))
	}
	
	# Get the first row with metaData
	colnames <- .jcall(context, "[S", "getTitleRow")
	numc <- length(colnames)
	
	matData <- list()
	rownames <- vector()
	
	for(i in 1:numc){
		matData[[i]] <- rep(NA, numr)
	}
	
	# now extract data
	for( i in 0:(numr-1) ){ # for each row
		row <- .jcall(context, "[Ljava/lang/Object;", "getRow", as.integer(i))
		for( x in 1:(numc) ){
			if(.jinstanceof(row[[x]], "java/lang/Float")){
				dta <- .jcall(row[[x]], "D", "doubleValue")
			}else if(.jinstanceof(row[[x]], "java/lang/Integer")){
				dta <- .jcall(row[[x]], "I", "intValue")
			}else{ # String
				dta <- .jstrVal(row[[x]])
			}
			matData[[x]][[i+1]] <- dta[1]
		}
		rownames[length(rownames)+1] <- i+1
	}

	# Now build the data frame
	mat <- data.frame(matData)
	names(mat) <- colnames
	return(mat)
}

taQuery <- function(context, query){
	numr <- .jcall(context, "I", "rawQuery", query)
	return(taProcessQuery(context, numr))
}

source("RCode/inc.r")





