var  tree_size = treeParams.getTreeSize(),
  maxLevel = treeParams.getMaxLevel(),
  rot = treeParams.getRotationAngle(),
  lenRand = treeParams.getLengthRand(),
  branchProb = treeParams.getBranchProb(),
  rotRand = treeParams.getRotationRand(),
  leafProb = treeParams.getLeafProb();

var progress = 1,
  growing = false,
  randSeed = treeParams.getSeed(),
  paramSeed = Math.floor(Math.random() * 1000),
  randBias = 0;

function plantTree() {
  background(255, 255, 255);
  randSeed = treeParams.getSeed();
  startGrow();
}

function restoreTree() {
    while(progress <= treeParams.getOldProgress())
        step();
}

function setup() {
  createCanvas(window.innerWidth, window.innerHeight);
  background(255, 255, 255);
  readInputs(false);
  startGrow();
}

function readInputs(updateTree) {
  if (updateTree && !growing) {
    progress = maxLevel + 1;
    loop();
  }
}

function windowResized() {
  resizeCanvas(windowWidth, windowHeight);
}

function draw() {
    //branch color
  stroke(114, 92, 66);

  translate(width / 2, height);
  scale(1, -1);

  translate(0, 20);

  branch(1, randSeed);
  noLoop();
}

function branch(level, seed) {
  if (progress < level) return;

  randomSeed(seed);

  var seed1 = random(1000),
    seed2 = random(1000);

  var growthLevel = progress - level > 1 || progress >= maxLevel + 1 ? 1 : progress - level;

  strokeWeight(20 * Math.pow((maxLevel - level + 1) / maxLevel, 2));

  var len = growthLevel * tree_size * (1 + rand2() * lenRand);

  line(0, 0, 0, len / level);
  translate(0, len / level);

  var doBranch1 = rand() < branchProb;
  var doBranch2 = rand() < branchProb;

  var doLeaves = rand() < leafProb;

  if (level < maxLevel) {
    var r1 = rot * (1 + rrand() * rotRand);
    var r2 = -rot * (1 - rrand() * rotRand);

    if (doBranch1) {
      push();
      rotate(r1);
      branch(level + 1, seed1);
      pop();
    }
    if (doBranch2) {
      push();
      rotate(r2);
      branch(level + 1, seed2);
      pop();
    }
  }

  if ((level >= maxLevel || (!doBranch1 && !doBranch2)) && doLeaves) {
    var p = Math.min(1, Math.max(0, progress - level));

    var flowerSize = (tree_size / 100) * p * (1 / 6) * (len / level) + 10;

    strokeWeight(5);
    //leaf color
    var r = 92, g = 169, b = 4;
    stroke(r + 40 * rand2(), g + 40 * rand2(), b+ 40 * rand2());
    rotate(-PI);
    for (var i = 0; i <= 5; i++) {
      line(0, 0, 0, flowerSize * (1 + 0.5 * rand2()));
      rotate((2 * PI) / 8);
    }
  }
}

function startGrow() {
  growing = true;
  progress = 1;
  grow();
}

function step() {
  if (growing) {
    progress += 1;//(maxLevel / 9);
    grow();
  }
}

function grow() {
  if (progress > maxLevel + 3) {
    progress = maxLevel + 3;
    loop();
    growing = false;
    return;
  }

  var startTime = millis();
  loop();
  var diff = millis() - startTime;

//  progress += 1;//((maxLevel / 8) * Math.max(diff, 20)) / 1000;
  treeParams.setProgress(progress);
//  setTimeout(grow, Math.max(1, 20 - diff));
}

function rand() {
  return random(1000) / 1000;
}

function rand2() {
  return random(2000) / 1000 - 1;
}

function rrand() {
  return rand2() + randBias;
}
