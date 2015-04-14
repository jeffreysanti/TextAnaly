

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

