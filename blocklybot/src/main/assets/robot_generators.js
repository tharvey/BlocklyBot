'use strict';

Blockly.JavaScript['robot_move_forward_internal'] = function(block) {
  var value = block.getFieldValue('VALUE');
  return 'Robot("moveForward",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_move_backward_internal'] = function(block) {
  var value = block.getFieldValue('VALUE');
  return 'Robot("moveBackward",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn_left_internal'] = function(block) {
  var value = block.getFieldValue('VALUE');
  return 'Robot("turnLeft",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn_right_internal'] = function(block) {
  var value = block.getFieldValue('VALUE');
  return 'Robot("turnRight",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_move_forward'] = function(block) {
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("moveForward",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_move_backward'] = function(block) {
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("moveBackward",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn_left'] = function(block) {
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("turnLeft",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_turn_right'] = function(block) {
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("turnRight",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_emote'] = function(block) {
  var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_NONE) || '0';
  return 'Robot("' + block.getFieldValue('TYPE') + '",' + value + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['robot_stop'] = function(block) {
  return 'Robot("stop");\n';
};

Blockly.JavaScript['robot_repeat_internal'] = Blockly.JavaScript['controls_repeat'];

Blockly.JavaScript['audio_play'] = function(block) {
  return 'Audio("' + block.getFieldValue('SOUND') + '", \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['audio_note'] = function(block) {
  var note = block.getFieldValue('NOTE');
  var dur = block.getFieldValue('TIME');
  return 'Note("' + note + '",' + dur + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['audio_note_dur'] = function(block) {
};

Blockly.JavaScript['audio_tone'] = function(block) {
  var freq = block.getFieldValue('FREQ');
  var dur = block.getFieldValue('TIME');
  return 'Tone(' + freq + ',' + dur + ', \'block_id_' + block.id + '\');\n';
};

Blockly.JavaScript['wait_time'] = function(block) {
  var dur = block.getFieldValue('TIME');
  return 'Wait(' + dur + ', \'block_id_' + block.id + '\');\n';
};

/* Assign my control blocks to the ones already defined in blocklylib-core */
Blockly.JavaScript['control_repeat'] = Blockly.JavaScript['controls_repeat'];

/*
 * Event Handlers
 */
Blockly.JavaScript['start'] = function(block) {
    var value = Blockly.JavaScript.statementToCode(block, 'FUNC')
    return 'function start() {\n' + value + '};\n';
};

Blockly.JavaScript['speech_speak'] = function(block) {
  return 'Speak("' + block.getFieldValue('TEXT') + '", \'block_id_' + block.id + '\');\n';
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
