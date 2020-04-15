# Script R : apply a SVD on a sparse matrix
# Following package have to be installed : install.packages("Matrix","irlba")
# 
# Script arguments :
#   - size : file with 3 integer : matrix row,column + non zero count
#   - rowIndexes : 
#   - columnIndexes : 
#   - values : 
#   - output :
#   - svdTargetSize : 

library('Matrix')
library('irlba')

args = commandArgs(trailingOnly=TRUE)

sizeFile = file(args[1], "rb")
matrixSize <- readBin(sizeFile, integer(), n = 2, size = 4, endian = "big")
nonZeroCount <- readBin(sizeFile, integer(), n = 1, size = 4, endian = "big")
close(sizeFile)
rm(sizeFile)
cat("Matrix size read, ",nonZeroCount,"non zero count, size",matrixSize[1],"x",matrixSize[2],"\n")

rowIndexFile = file(args[2], "rb")
rowIndexes <-readBin(rowIndexFile, integer(), n = nonZeroCount, size = 4, endian = "big")
close(rowIndexFile)
rm(rowIndexFile)
cat("Row indexes read\n")

columnIndexFile = file(args[3], "rb")
columnIndexes <-readBin(columnIndexFile, integer(), n = nonZeroCount, size = 4, endian = "big")
close(columnIndexFile)
rm(columnIndexFile)
cat("Column indexes read\n")

valuesFile = file(args[4], "rb")
matrixValues <-readBin(valuesFile, integer(), n = nonZeroCount, size = 4, endian = "big")
close(valuesFile)
rm(valuesFile)
cat("Matrix values read\n")

A <- sparseMatrix(rowIndexes,columnIndexes, x = matrixValues,dims = matrixSize)
cat("A matrix created\n")
rm(matrixValues, matrixSize, nonZeroCount, columnIndexes, rowIndexes)
cat("Cleanup finished, svd computing will be launched via irlba\n")

svdResult <- irlba(A, as.numeric(args[6]))$u
cat("SVD computed\n")

outputFile = file(args[5], "wb")
writeBin(c(nrow(svdResult),ncol(svdResult)),outputFile,size = 4,endian = "big")
for(row in 1:nrow(svdResult)) {
  writeBin(svdResult[row,],outputFile,size = 8,endian = "big")
}
close(outputFile)
rm(outputFile, row,A)
cat("Result saved\n")