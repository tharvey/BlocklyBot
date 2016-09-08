'use strict';

// Extensions to Blockly's language and JavaScript generator.
Blockly.JavaScript['robot_move_internal'] = function(block) {
  // Generate JavaScript for moving forward or backwards.
  var value = block.getFieldValue('VALUE');
  return 'Robot("' + block.getFieldValue('DIR') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn_internal'] = function(block) {
  // Generate JavaScript for turning left or right.
  var value = block.getFieldValue('VALUE');
  return 'Robot("' + block.getFieldValue('DIR') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_move'] = function(block) {
  // Generate JavaScript for moving forward or backwards.
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE',
      Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("' + block.getFieldValue('DIR') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn'] = function(block) {
  // Generate JavaScript for turning left or right.
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE',
      Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("' + block.getFieldValue('DIR') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_misc'] = function(block) {
  // Generate JavaScript for turning left or right.
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE',
      Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("' + block.getFieldValue('TYPE') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_repeat_internal'] = Blockly.JavaScript['controls_repeat'];

// Extensions to Blockly's language and JavaScript generator.
Blockly.JavaScript['speech_speak'] = function(block) {
  return 'Speak("' + block.getFieldValue('TEXT') + '", \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['audio_play_internal'] = function(block) {
  return 'Audio("' + block.getFieldValue('SOUND') + '", \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['audio_play'] = function(block) {
  return 'Audio("' + block.getFieldValue('SOUND') + '", \'block_id_' + block.id + '\');\n';
};

/*
 * Event Handlers
 */
Blockly.JavaScript['start'] = function(block) {
    var value = Blockly.JavaScript.statementToCode(block, 'FUNC')
    return 'function start() {\n' + value + '};\n';
};

Blockly.JavaScript['speech_listen'] = function(block) {
    var value = Blockly.JavaScript.statementToCode(block, 'FUNC')
    return 'Listen("' + block.getFieldValue('CMD') + '", function() {\n' + value + '});';
};

Blockly.JavaScript['speech_listen_text'] = function(block) {
    var value = Blockly.JavaScript.statementToCode(block, 'FUNC')
    return 'Listen("' + block.getFieldValue('CMD') + '", function() {\n' + value + '});';
};

/**
 * The generated code for robot blocks includes block ID strings.  These are useful for
 * highlighting the currently running block, but that behaviour is not supported in Android Blockly
 * as of May 2016.  This snippet generates the block code normally, then strips out the block IDs
 * for readability when displaying the code to the user.
 *
 * Post-processing the block code in this way allows us to use the same generators for the Android
 * and web versions of the robot.
 */
Blockly.JavaScript.workspaceToCodeWithId = Blockly.JavaScript.workspaceToCode;

Blockly.JavaScript.workspaceToCode = function(workspace) {
  var code = this.workspaceToCodeWithId(workspace);
  // Strip out block IDs for readability.
  code = goog.string.trimRight(code.replace(/(,\s*)?'block_id_[^']+'\)/g, ')'))
  return code;
};
