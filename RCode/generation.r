

taMarkov <- function(context, bgramProb=0, grammerFix=FALSE, sub=FALSE, subNoun=0.2, subAdj=0.2){
	c <- .jnew("net/jeffreysanti/text_analy4/MarkovGen", context, bgramProb)

	if(sub){
		.jcall(c, "V", "enableWordSubstituer", subNoun, subAdj)
		print("Enabled Word Substitution")
	}
	if(grammerFix){
		.jcall(c, "V", "enableGrammarVerifier", TRUE)
		print("Enabled Auto Grammer Correction")
	}
	return(c)
}

taMarkovGen <- function(mc, count=1){
	rows <- vector()
	for( i in 1:(count) ){
		sent <- .jcall(mc, "Ljava/lang/String;", "genText")
		rows[length(rows)+1] <- sent
	}
	return(rows)
}

taMarkovGenStarting <- function(mc, word1, word2="", count=1){
	rows <- vector()
	for( i in 1:(count) ){
	     if(word2 == ""){
	     	sent <- .jcall(mc, "Ljava/lang/String;", "genTextStarting", word1)
	     }else{
		sent <- .jcall(mc, "Ljava/lang/String;", "genTextStarting", word1, word2)
	     }
	     rows[length(rows)+1] <- sent
	}
	return(rows)
}


taMarkovGenContaining <- function(mc, word, count=1, timeout=500){
	rows <- vector()
	for( i in 1:(count) ){
	     sent <- .jcall(mc, "Ljava/lang/String;", "genContaining", word, as.integer(timeout))
	     rows[length(rows)+1] <- sent
	}
	return(rows)
}


