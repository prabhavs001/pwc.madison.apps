var gulp = require('gulp');
var sass = require('gulp-sass');

gulp.task('default', function() {
  if(process.env.NODE_ENV === 'production') {
    console.log('gulp runs as expected on production');
  } else {
    console.log('gulp runs as expected');
  }
});

gulp.task('styles', function() {
    console.log('*********STARTING TO PROCESS THE GULP COMPILATION FOR ViewPoint*******');
    gulp.src(['front-end-styles-vp/scss/**/*.scss'])
        .pipe(sass().on('error', sass.logError))
        .pipe(gulp.dest('src/main/content/jcr_root/apps/pwc-madison/clientlibs/clientlib-site-vp/css'));
});

// Gulp watch syntax
//gulp.watch('front-end-styles/scss/**/*.scss', ['styles']);
