
taSort <- function(mat, cols){
	sort=vector()
	for(i in 1:length(cols)){
		if(substr(cols[i], 1, 1) == "-"){
			if(! substring(cols[i], 2) %in% colnames(mat)){
				print(paste("Non-Existant Column Name: ",substring(cols[i], 2)))
				return(mat)
			}
			sort[length(sort)+1] <- -mat[substring(cols[i], 2)]
		}else{
			if(! cols[i] %in% colnames(mat)){
				print(paste("Non-Existant Column Name: ",cols[i]))
				return(mat)
			}
			sort[length(sort)+1] <- mat[cols[i]]
		}
	}
	return (mat[do.call(order, sort), ])
}

taFilterAtLeast <- function(mat, colname, minVal){
	if(! colname %in% colnames(mat)){
		print(paste("Non-Existant Column Name: ",colname))
		return(mat)
	}
	return (mat[mat[colname] >= minVal , ])
}
taFilterAtMost <- function(mat, colname, maxVal){
	if(! colname %in% colnames(mat)){
		print(paste("Non-Existant Column Name: ",colname))
		return(mat)
	}
	return (mat[mat[colname] <= maxVal , ])
}
taFilterEqual <- function(mat, colname, val){
	if(! colname %in% colnames(mat)){
		print(paste("Non-Existant Column Name: ",colname))
		return(mat)
	}
	return (mat[mat[[colname]] %in% val , ])
}
taFilterNotEqual <- function(mat, colname, val){
	if(! colname %in% colnames(mat)){
		print(paste("Non-Existant Column Name: ",colname))
		return(mat)
	}
	return (mat[ !(mat[[colname]] %in% val) , ])
}

has_required_cols <- function(mat, cols){
	for(i in 1:length(cols)){
		if( ! ( cols[i] %in% colnames(mat) ) ){
			print("DataFrame must have following columns:")
			print(cols)
			return(FALSE)
		}
	}
	return(TRUE)
}

taAppendInfo <- function(mat, param){
	if(param == "lexdens"){
		if(has_required_cols(mat, c("lextokans", "wcnt"))){
			mat["lexdens"] <- apply(mat,1,function(row) row["lextokans"]/row["wcnt"])
		}
	}else if(param == "gfog"){
		if(has_required_cols(mat, c("scnt", "wcnt", "complexwrds"))){
			mat["gfog"] <- apply(mat,1,function(row) 0.4*((row["wcnt"]/row["scnt"]) + row["complexwrds"]/row["wcnt"]))
		}
	}else if(param == "flki"){
		if(has_required_cols(mat, c("scnt", "wcnt", "syl"))){
			mat["flki"] <- apply(mat,1,function(row) -15.9 + 0.39*(row["wcnt"]/row["scnt"]) + 11.8*(row["syl"]/row["wcnt"]))
		}
	}else{
		print("Unkown param specified!")
	}
	return(mat)
}

taAppendLexicalDensity <- function(mat){return(taAppendInfo(mat, "lexdens"))}
taAppendGunningFogIndex <- function(mat){return(taAppendInfo(mat, "gfog"))}
taAppendFleschKincaidGradeLevel <- function(mat){return(taAppendInfo(mat, "flki"))}




