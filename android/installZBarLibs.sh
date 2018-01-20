
git clone https://github.com/chentao0707/ZBarAndroidSDK.git

cur_dir=`pwd`

pushd ZBarAndroidSDK/ZBarScanProjAll/libs

for d in */ ; do
  echo "Copying ${d}libZBarDecoder.so to ${cur_dir}/app/src/main/jniLibs/${d}libZBarDecoder.so"
  echo "Copying ${d}libiconv.so to ${cur_dir}/app/src/main/jniLibs/${d}libiconv.so"
  cp ${d}libZBarDecoder.so ${cur_dir}/app/src/main/jniLibs/${d}libZBarDecoder.so
  cp ${d}libiconv.so ${cur_dir}/app/src/main/jniLibs/${d}libiconv.so
done

popd

mkdir ./includeLibs/ZBar
cp ZBarAndroidSDK/ZBarBuild/jni/include/zbar.h ./includeLibs/ZBar
cp -r ZBarAndroidSDK/ZBarBuild/jni/include/zbar ./includeLibs/ZBar

echo 'Removing ./ZBarAndroidSDK'
rm -rf ./ZBarAndroidSDK