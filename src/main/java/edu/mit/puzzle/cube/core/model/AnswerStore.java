package edu.mit.puzzle.cube.core.model;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnswerStore {
    private final Map<String, Answer> puzzleAnswers;

    public AnswerStore(List<Answer> answerList) {
        puzzleAnswers = answerList.stream().collect(
                Collectors.toMap(Answer::getPuzzleId, Function.identity())
        );
    }

    public Answer getAnswer(String puzzleId) {
        Answer answer = puzzleAnswers.get(puzzleId);
        if (answer == null) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    String.format("Unknown puzzle id %s", puzzleId));
        }
        return answer;
    }
}
