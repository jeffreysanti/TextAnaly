
taColumnToAggregate <- function(col){
	n = 0
	for(i in 1:length(col)){
		n = n + col[i]
		col[i] <- n
	}
	return(col)
}

taSentiment <- function(context, bySentence=TRUE, sec=list()){
	numr <- .jcall(context, "I", "sentiment", bySentence, .jarray(as.integer(sec)))
	mat <- taProcessQuery(context, numr)
	
	for(i in 1:length(names(mat))){
		names(mat)[[i]] <- gsub("/wcnt", "", names(mat)[[i]])
	}
	return(mat)
}

taSectionSentiment <- function(context, sec=list()){
	numr <- .jcall(context, "I", "sentiment", FALSE, .jarray(as.integer(sec)))
	mat <- taProcessQuery(context, numr)
	
	for(i in 1:length(names(mat))){
		names(mat)[[i]] <- gsub("/wcnt", "", names(mat)[[i]])
	}
	return(mat)
}


taWordDist <- function(context, words, aggregate=FALSE){
	numr <- .jcall(context, "I", "wordDist", .jarray(as.character(words)), aggregate)
	mat <- taProcessQuery(context, numr)
	return(mat)
}

taWordsByCount <- function(context, stoplist=list(), sec=list()){
	numr <- .jcall(context, "I", "wordsByCount_StopList", .jarray(as.character(stoplist)), .jarray(as.integer(sec)))
	mat <- taProcessQuery(context, numr)
	names(mat)[names(mat)=="SUM(worddist.cnt)"] <- "count"
	names(mat)[names(mat)=="cnt"] <- "count"
	return(mat)
}
taListedWordsByCount <- function(context, wordList, sec=list()){
	numr <- .jcall(context, "I", "wordsByCount_ByList", .jarray(as.character(wordList)), .jarray(as.integer(sec)))
	mat <- taProcessQuery(context, numr)
	names(mat)[names(mat)=="SUM(worddist.cnt)"] <- "count"
	names(mat)[names(mat)=="cnt"] <- "count"
	return(mat)
}

taSections <- function(context, sec=list()){
	numr <- .jcall(context, "I", "sectionData", .jarray(as.integer(sec)))
	mat <- taProcessQuery(context, numr)
	# convert to more consistant naming
	names(mat)[names(mat)=="lextoks"] <- "lextokans"
	names(mat)[names(mat)=="unqwords"] <- "unqWords"
	names(mat)[names(mat)=="cwords"] <- "complexwrds"
	names(mat)[names(mat)=="syls"] <- "syl"
	return(mat)
}

taSentencesBySection <- function(context, sec, showText=FALSE, sent=list()){
	numr <- .jcall(context, "I", "sentenceData", as.integer(sec), showText, .jarray(as.integer(sent)))
	mat <- taProcessQuery(context, numr)
	mat["scnt"] <- apply(mat,1,function(row) 1)
	mat <- data.frame(c(mat[1:2],mat[length(mat)], mat[3:(length(mat)-1)]))
	return(mat)
}

toSecSentWordPair <- function(lst){
	out = list()
	if(is.data.frame(lst)){
		if(! ("sent" %in% colnames(lst) && "sec" %in% colnames(lst)) ){
			print("Data.frame must have a sent and sec column!")
			return(NULL)
		}
		for(i in 1:length(lst[["sent"]])){
			out[[i]] <- paste(lst[i, "sec"], ":", lst[i, "sent"], sep="")
		}
	}else{
		out = lst
	}
	out <- as.character(out)
	return(out)
}

taSentence <- function(context, lst){
	out <- 	toSecSentWordPair(lst)
	if(is.null(out)){
		return(NULL)
	}
	numr <- .jcall(context, "I", "sentenceDataChoose", .jarray(out))
	mat <- taProcessQuery(context, numr)
	mat["scnt"] <- apply(mat,1,function(row) 1)
	mat <- data.frame(c(mat[1:2],mat[length(mat)], mat[3:(length(mat)-1)]))
	return(mat)
}
taAllSentences <- function(context){
	numr <- .jcall(context, "I", "sentenceDataAll")
	mat <- taProcessQuery(context, numr)
	mat["scnt"] <- apply(mat,1,function(row) 1)
	mat <- data.frame(c(mat[1:2],mat[length(mat)], mat[3:(length(mat)-1)]))
	return(mat)
}

taSentenceSentiment <- function(context, lst){
	out <- 	toSecSentWordPair(lst)
	if(is.null(out)){
		return(NULL)
	}
	numr <- .jcall(context, "I", "sentenceSentiment", .jarray(out))
	mat <- taProcessQuery(context, numr)
	for(i in 1:length(names(mat))){
		names(mat)[[i]] <- gsub("/wcnt", "", names(mat)[[i]])
	}
	return(mat)
}

taRelationsSummary <- function(context, words, secs=list()){
	if(length(words) < 1){
		print("Words must be non-empty")
		return(NULL)
	}
	numr <- .jcall(context, "I", "relationsSummary", .jarray(as.character(words)), .jarray(as.integer(secs)))
	mat <- taProcessQuery(context, numr)
	print(paste(numr, " Results Returned"))
	return(mat)
}

taRelations <- function(context, words1, words2, sections=list()){
	if(length(words1) < 1 || length(words2) < 1){
		print("Words must be non-empty")
		return(NULL)
	}
	numr <- .jcall(context, "I", "relations", .jarray(as.character(words1)), .jarray(as.character(words2)), .jarray(as.integer(sections)))
	mat <- taProcessQuery(context, numr)
	print(paste(numr, " Results Returned"))
	return(mat)
}

taLocateTaggedSentences <- function(context, tag, secs=list()){
	numr <- .jcall(context, "I", "sentencesWithTag", as.character(tag), .jarray(as.integer(secs)))
	mat <- taProcessQuery(context, numr)
	return(mat)
}

taSectionsTagValue <- function(context, tag){
	numr <- .jcall(context, "I", "getSectionTagValues", .jarray(as.character(tag)))
	mat <- taProcessQuery(context, numr)
	return(mat)
}

taLocateWordUsage <- function(context, words, secs=list(), containsAll=FALSE){
	if(length(words) < 1){
		print("Words must be non-empty")
		return(NULL)
	}
	numr <- .jcall(context, "I", "wordLocation", .jarray(as.character(words)), .jarray(as.integer(secs)), containsAll)
	mat <- taProcessQuery(context, numr)
	return(mat)
}

taAlternateWords <- function(context, words){
	if(length(words) < 1){
		print("Words must be non-empty")
		return(NULL)
	}
	numr <- .jcall(context, "I", "alternateWords", .jarray(as.character(words)))
	mat <- taProcessQuery(context, numr)
	return(mat)
}

taStopList <- function(){
	inlist <- read.table("RCode/stoplist.txt")
	inlist <- inlist$V1
	return(inlist)
}

taBiGrams <- function(context, word){
	numr <- .jcall(context, "I", "bigrams", word)
	mat <- taProcessQuery(context, numr)
	names(mat)[names(mat)=="SUM(cnt)"] <- "count"
	probs <- mat$count / sum(mat$count)
	mat["prob"] <- probs
	return(mat)
}

taTriGrams <- function(context, word1, word2=""){
	if(word2 == ""){
		numr <- .jcall(context, "I", "trigrams", word1)
		mat <- taProcessQuery(context, numr)
		names(mat)[names(mat)=="SUM(cnt)"] <- "count"
		probs <- mat$count / sum(mat$count)
		mat["prob"] <- probs
		return(mat)
	}else{
		numr <- .jcall(context, "I", "trigrams", word1, word2)
		mat <- taProcessQuery(context, numr)
		names(mat)[names(mat)=="SUM(cnt)"] <- "count"
		probs <- mat$count / sum(mat$count)
		mat["prob"] <- probs
		return(mat)
	}
}

taSentenceStart <- function(context){
	numr <- .jcall(context, "I", "firstWords")
	mat <- taProcessQuery(context, numr)
	names(mat)[names(mat)=="SUM(cnt)"] <- "count"
	probs <- mat$count / sum(mat$count)
	mat["prob"] <- probs
	return(mat)
}









