

taWordDistNormalized <- function(context, words, aggregate=FALSE){
	usage <- taWordDist(context, words, aggregate)
	totalQuant <- taSections(context)
	if(aggregate){
		n = 0
		for(i in 1:length(totalQuant$wcnt)){
			n = n + totalQuant$wcnt[i]
			totalQuant$wcnt[i] <- n
		}
	}
	usage <-  usage$cnt / totalQuant$wcnt
	mat <- list()
	mat[[1]] <- totalQuant$sec
	mat[[2]] <- usage
	mat <- data.frame(mat)
	names(mat) <- c("sec", "usage")
	return(mat)
}

taSectionDate <- function(context){
	dateMatrix <- taSectionsTagValue(context, c("YEAR", "MONTH", "DAY"))
	dv <- vector()
	for(i in 1:length(dateMatrix$YEAR)){
		dv[i] <- paste(dateMatrix$YEAR[i], "/", dateMatrix$MONTH[i], "/", dateMatrix$DAY[i], sep="")
	}
	mat <- list()
	mat[[1]] <- dateMatrix$sec
	mat[[2]] <- as.Date(dv, format="%Y/%m/%d")
	mat <- data.frame(mat)
	names(mat) <- c("sec", "date")
	return(mat)
}

taPlotByDate <- function(dates, other){
	plot(dates, other, xaxt="n")
	axis.Date(1, at=seq(min(dates), max(dates), by="month"), format="%b,%y")
}
